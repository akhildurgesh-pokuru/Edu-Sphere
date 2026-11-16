import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';

export function useProviders() {
  return useQuery({
    queryKey: ['providers'],
    queryFn: async () => (await api.get('/auth/providers')).data,
    staleTime: 10 * 60 * 1000,
    retry: false,
  });
}
